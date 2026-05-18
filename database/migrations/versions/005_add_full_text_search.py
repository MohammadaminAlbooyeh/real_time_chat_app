from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = "005_add_full_text_search"
down_revision: Union[str, None] = "004_add_message_attachments"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_index(
        "idx_messages_content_fts",
        "messages",
        [sa.text("to_tsvector('english', content)")],
        postgresql_using="gin",
    )


def downgrade() -> None:
    op.drop_index("idx_messages_content_fts")
