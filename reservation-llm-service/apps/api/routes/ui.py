"""UI 관련 라우터."""

from pathlib import Path

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent
templates = Jinja2Templates(directory=str(BASE_DIR / "webapps/templates"))

router = APIRouter(tags=["UI"])


@router.get("/", response_class=HTMLResponse)
async def read_root(request: Request) -> HTMLResponse:

    return templates.TemplateResponse("index.html", {"request": request})