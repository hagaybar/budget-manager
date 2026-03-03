"""FastAPI application entry point for Budget Manager API."""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from app.database import create_tables
from app.routers import transactions, summary


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan: create tables on startup."""
    create_tables()
    yield


app = FastAPI(
    title="Budget Manager API",
    description="A RESTful API for tracking personal income and expenses.",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS middleware - allow all origins for development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(transactions.router)
app.include_router(summary.router)


@app.get("/")
def root():
    """Serve the Budget Manager UI."""
    return FileResponse("app/static/index.html")


# Mount static files AFTER routers so API routes take priority
app.mount("/static", StaticFiles(directory="app/static"), name="static")
