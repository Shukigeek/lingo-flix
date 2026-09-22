from sqlalchemy import Column, Integer, String, Float, ForeignKey, Table
from sqlalchemy.orm import relationship
from backend.app.db.base_class import Base

class Movie(Base):
    __tablename__ = "movies"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    original_title = Column(String)
    description = Column(String)
    year = Column(Integer)
    rating = Column(Float)
    poster_path = Column(String)
    file_path = Column(String) # Path on the Raspberry Pi
    difficulty_score = Column(Float) # Calculated by AI

    # Relationships
    genres = relationship("Genre", secondary="movie_genres")
    cast = relationship("Actor", secondary="movie_cast")

class Actor(Base):
    __tablename__ = "actors"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    profile_path = Column(String)

class Genre(Base):
    __tablename__ = "genres"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True)

movie_genres = Table(
    "movie_genres",
    Base.metadata,
    Column("movie_id", Integer, ForeignKey("movies.id")),
    Column("genre_id", Integer, ForeignKey("genres.id")),
)

movie_cast = Table(
    "movie_cast",
    Base.metadata,
    Column("movie_id", Integer, ForeignKey("movies.id")),
    Column("actor_id", Integer, ForeignKey("actors.id")),
    Column("character_name", String),
)
