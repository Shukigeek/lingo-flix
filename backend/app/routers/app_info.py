from fastapi import APIRouter

from ..config import get_settings
from ..schemas import AppVersionOut

router = APIRouter(prefix="/app", tags=["meta"])


@router.get("/version", response_model=AppVersionOut)
async def get_app_version():
    """Public, no-auth endpoint the Android app polls to see if a newer build exists."""
    settings = get_settings()
    return AppVersionOut(
        latest_version_code=settings.app_latest_version_code,
        latest_version_name=settings.app_latest_version_name,
        download_url=settings.app_download_url,
        update_notes=settings.app_update_notes,
        force_update=settings.app_force_update,
    )
