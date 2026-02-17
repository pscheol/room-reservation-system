import gzip
import logging
import logging.config
import shutil
from logging.handlers import TimedRotatingFileHandler
from pathlib import Path
from typing import Optional

from apps.config.settings import settings


class CompressedTimedRotatingFileHandler(TimedRotatingFileHandler):
    """
    로테이션된 로그 파일을 자동으로 gzip 압축하는 핸들러

    TimedRotatingFileHandler를 상속받아 doRollover() 메서드를 오버라이드하여
    로테이션 후 이전 파일을 gzip으로 압축합니다.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def doRollover(self) -> None:
        """
        로그 파일 로테이션 실행 및 이전 파일 압축

        부모 클래스의 doRollover()를 호출한 후,
        생성된 백업 파일을 찾아서 gzip으로 압축합니다.
        """
        # 부모 클래스의 로테이션 로직 실행
        super().doRollover()

        # 로테이션된 파일 압축 (가장 최근 백업 파일)
        # TimedRotatingFileHandler는 baseFilename + suffix 형태로 파일명 생성
        log_dir = Path(self.baseFilename).parent
        log_basename = Path(self.baseFilename).name

        # 로테이션된 파일 찾기 (예: app.log.2024-01-15)
        for log_file in sorted(log_dir.glob(f"{log_basename}.*")):
            if not str(log_file).endswith(".gz"):
                self._compress_file(log_file)

    def _compress_file(self, source_file: Path) -> None:
        """
        파일을 gzip으로 압축

        Args:
            source_file: 압축할 원본 파일 경로
        """
        compressed_file = Path(f"{source_file}.gz")

        try:
            with open(source_file, "rb") as f_in:
                with gzip.open(compressed_file, "wb") as f_out:
                    shutil.copyfileobj(f_in, f_out)

            # 압축 성공 시 원본 파일 삭제
            source_file.unlink()
            print(f"로그 파일 압축 완료: {compressed_file}")
        except Exception as e:
            print(f"로그 파일 압축 실패: {source_file}, 에러: {e}")


def setup_logging(config_path: str = "log-config.ini") -> None:
    """
    log-config.ini 파일을 사용하여 로깅 시스템 초기화

    Args:
        config_path: 로그 설정 파일 경로 (기본값: log-config.ini)

    Raises:
        FileNotFoundError: 설정 파일이 존재하지 않을 경우
    """
    # logs 디렉토리 생성 (없으면)
    logs_dir = Path("logs")
    logs_dir.mkdir(exist_ok=True)

    # 설정 파일 존재 확인
    if not Path(config_path).exists():
        raise FileNotFoundError(f"로그 설정 파일을 찾을 수 없습니다: {config_path}")

    # 로깅 설정 로드
    logging.config.fileConfig(config_path, disable_existing_loggers=False)


def setup_logger(name: Optional[str] = None) -> logging.Logger:
    """
    로거 인스턴스 생성 및 반환

    Args:
        name: 로거 이름 (기본값: None, 루트 로거 반환)

    Returns:
        설정된 로거 인스턴스
    """
    logger = logging.getLogger(name)
    return logger


# 애플리케이션 시작 시 로깅 시스템 초기화
try:
    setup_logging()
except FileNotFoundError as e:
    # 설정 파일이 없으면 기본 콘솔 로거 사용
    print(f"경고: {e}")
    print("기본 콘솔 로거를 사용합니다.")
    logging.basicConfig(
        level=getattr(logging, settings.log_level.upper()),
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

# 기본 로거
logger = setup_logger(__name__)