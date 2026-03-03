"""Pydantic v2 request/response models for the Budget Manager API."""

from typing import Literal, Optional
from pydantic import BaseModel, Field, field_validator, model_validator


class TransactionCreate(BaseModel):
    """Schema for creating a new transaction."""
    type: Literal["income", "expense"]
    amount: float = Field(..., gt=0, description="Transaction amount, must be > 0")
    category: str = Field(..., min_length=1, max_length=100)
    description: str = Field(default="", max_length=500)
    date: str = Field(..., description="Date in YYYY-MM-DD format")

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        """Validate that date is in YYYY-MM-DD format."""
        import re
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", v):
            raise ValueError("Date must be in YYYY-MM-DD format")
        # Verify it's a real date
        from datetime import date
        try:
            date.fromisoformat(v)
        except ValueError:
            raise ValueError("Invalid date value")
        return v


class TransactionUpdate(BaseModel):
    """Schema for updating an existing transaction. All fields optional."""
    type: Optional[Literal["income", "expense"]] = None
    amount: Optional[float] = Field(default=None, gt=0)
    category: Optional[str] = Field(default=None, min_length=1, max_length=100)
    description: Optional[str] = Field(default=None, max_length=500)
    date: Optional[str] = Field(default=None)

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        import re
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", v):
            raise ValueError("Date must be in YYYY-MM-DD format")
        from datetime import date
        try:
            date.fromisoformat(v)
        except ValueError:
            raise ValueError("Invalid date value")
        return v

    @model_validator(mode="after")
    def check_at_least_one_field(self) -> "TransactionUpdate":
        """Ensure at least one field is provided for update."""
        if all(
            getattr(self, field) is None
            for field in self.model_fields
        ):
            raise ValueError("At least one field must be provided")
        return self


class TransactionResponse(BaseModel):
    """Schema for returning a transaction."""
    id: int
    type: str
    amount: float
    category: str
    description: str
    date: str
    created_at: str


class CategoryBreakdown(BaseModel):
    """Category-level aggregation within a monthly summary."""
    category: str
    total: float
    count: int


class MonthlySummary(BaseModel):
    """Monthly income/expense summary response."""
    year: int
    month: int
    total_income: float
    total_expenses: float
    net_balance: float
    transaction_count: int
    categories: list[CategoryBreakdown]
