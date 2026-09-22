from fastapi import APIRouter, HTTPException, status
from sqlalchemy import or_, select

from ..deps import DB, CurrentUser
from ..models import User
from ..schemas import LoginRequest, RegisterRequest, TokenResponse, UserOut, UserUpdate
from ..security import create_access_token, hash_password, verify_password
from .common import refresh_streak, user_out

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register(body: RegisterRequest, db: DB):
    exists = await db.scalar(select(User).where(or_(User.email == body.email.lower(), User.username == body.username)))
    if exists:
        raise HTTPException(status.HTTP_409_CONFLICT, "Email or username already registered")
    user = User(
        email=body.email.lower(),
        username=body.username,
        password_hash=hash_password(body.password),
        display_name=body.display_name or body.username,
        native_language=body.native_language,
        target_language=body.target_language,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return TokenResponse(access_token=create_access_token(user.id), user=user_out(user))


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest, db: DB):
    ident = body.identifier.strip()
    user = await db.scalar(select(User).where(or_(User.email == ident.lower(), User.username == ident)))
    if user is None or not verify_password(body.password, user.password_hash):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Wrong email/username or password")
    refresh_streak(user)
    await db.commit()
    return TokenResponse(access_token=create_access_token(user.id), user=user_out(user))


@router.post("/guest", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def guest(db: DB):
    """Create an anonymous account so the app works without sign-up; can be upgraded later."""
    import secrets

    suffix = secrets.token_hex(4)
    user = User(
        email=f"guest_{suffix}@lingoflix.local",
        username=f"guest_{suffix}",
        password_hash=hash_password(secrets.token_urlsafe(16)),
        display_name="לומד",
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return TokenResponse(access_token=create_access_token(user.id), user=user_out(user))


@router.get("/me", response_model=UserOut)
async def me(user: CurrentUser, db: DB):
    refresh_streak(user)
    await db.commit()
    return user_out(user)


@router.patch("/me", response_model=UserOut)
async def update_me(body: UserUpdate, user: CurrentUser, db: DB):
    for k, v in body.model_dump(exclude_none=True).items():
        setattr(user, k, v)
    await db.commit()
    return user_out(user)


@router.post("/upgrade", response_model=TokenResponse)
async def upgrade_guest(body: RegisterRequest, user: CurrentUser, db: DB):
    """Turn a guest account into a real one, keeping XP and progress."""
    clash = await db.scalar(
        select(User).where(or_(User.email == body.email.lower(), User.username == body.username), User.id != user.id)
    )
    if clash:
        raise HTTPException(status.HTTP_409_CONFLICT, "Email or username already registered")
    user.email = body.email.lower()
    user.username = body.username
    user.password_hash = hash_password(body.password)
    user.display_name = body.display_name or body.username
    await db.commit()
    return TokenResponse(access_token=create_access_token(user.id), user=user_out(user))
