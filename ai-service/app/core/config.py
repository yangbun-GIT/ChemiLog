from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_env: str = "dev"

    internal_api_secret: str = "change_me_internal_secret"
    spring_main_internal_url: str = "http://spring-main:8081/api/v1/internal/logs"

    openai_api_key: str = ""
    openai_main_model: str = "gpt-4o"
    openai_policy_model: str = "gpt-4o-mini"
    openai_embedding_model: str = "text-embedding-3-small"

    redis_host: str = "redis"
    redis_port: int = 6379
    redis_semantic_cache_ttl_seconds: int = 1800

    milvus_host: str = "milvus"
    milvus_port: int = 19530
    milvus_collection_name: str = "additive_knowledge"
    rag_top_k: int = 3
    rag_distance_threshold: float = 0.4

    minio_endpoint: str = "minio:9000"
    minio_access_key: str = "chemilog"
    minio_secret_key: str = "change_me_minio_password"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


settings = Settings()
