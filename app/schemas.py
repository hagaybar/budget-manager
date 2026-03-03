"""Pydantic v2 request/response models for the Budget Manager API."""

from typing import Literal, Optional
from pydantic import BaseModel, Field, field_validator, model_validator
from datetime import date as date_type


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
        try:
            date_type.fromisoformat(v)
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
        try:
            date_type.fromisoformat(v)
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
    recurring_id: Optional[int] = None


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


# ---------------------------------------------------------------------------
# Recurring Transaction Schemas
# ---------------------------------------------------------------------------

class RecurringTransactionCreate(BaseModel):
    """Schema for creating a new recurring transaction definition."""
    type: Literal["income", "expense"]
    amount: float = Field(..., gt=0, description="Recurring amount, must be > 0")
    category: str = Field(..., min_length=1, max_length=100)
    description: str = Field(default="", max_length=500)
    frequency: Literal["weekly", "monthly"]
    day_of_week: Optional[int] = Field(default=None, ge=0, le=6,
        description="0=Monday..6=Sunday, required when frequency='weekly'")
    day_of_month: Optional[int] = Field(default=None, ge=1, le=31,
        description="1-31, required when frequency='monthly'")
    start_date: Optional[str] = Field(default=None,
        description="YYYY-MM-DD, defaults to today if omitted")
    end_date: Optional[str] = Field(default=None,
        description="YYYY-MM-DD, null means no end date")

    @field_validator("start_date", "end_date")
    @classmethod
    def validate_date_format(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        import re
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", v):
            raise ValueError("Date must be in YYYY-MM-DD format")
        try:
            date_type.fromisoformat(v)
        except ValueError:
            raise ValueError("Invalid date value")
        return v

    @model_validator(mode="after")
    def check_frequency_fields(self) -> "RecurringTransactionCreate":
        """Ensure the correct day field is provided for the chosen frequency."""
        if self.frequency == "weekly" and self.day_of_week is None:
            raise ValueError("day_of_week is required when frequency is 'weekly'")
        if self.frequency == "monthly" and self.day_of_month is None:
            raise ValueError("day_of_month is required when frequency is 'monthly'")
        if self.frequency == "weekly" and self.day_of_month is not None:
            raise ValueError("day_of_month must not be set when frequency is 'weekly'")
        if self.frequency == "monthly" and self.day_of_week is not None:
            raise ValueError("day_of_week must not be set when frequency is 'monthly'")
        return self


class RecurringTransactionUpdate(BaseModel):
    """Schema for updating a recurring transaction definition. All fields optional."""
    type: Optional[Literal["income", "expense"]] = None
    amount: Optional[float] = Field(default=None, gt=0)
    category: Optional[str] = Field(default=None, min_length=1, max_length=100)
    description: Optional[str] = Field(default=None, max_length=500)
    frequency: Optional[Literal["weekly", "monthly"]] = None
    day_of_week: Optional[int] = Field(default=None, ge=0, le=6)
    day_of_month: Optional[int] = Field(default=None, ge=1, le=31)
    start_date: Optional[str] = Field(default=None)
    end_date: Optional[str] = Field(default=None)
    is_active: Optional[int] = Field(default=None, ge=0, le=1)

    @field_validator("start_date", "end_date")
    @classmethod
    def validate_date_format(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        import re
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", v):
            raise ValueError("Date must be in YYYY-MM-DD format")
        try:
            date_type.fromisoformat(v)
        except ValueError:
            raise ValueError("Invalid date value")
        return v

    @model_validator(mode="after")
    def check_at_least_one_field(self) -> "RecurringTransactionUpdate":
        """Ensure at least one field is provided for update."""
        if all(
            getattr(self, field) is None
            for field in self.model_fields
        ):
            raise ValueError("At least one field must be provided")
        return self


class RecurringTransactionResponse(BaseModel):
    """Schema for returning a recurring transaction definition."""
    id: int
    type: str
    amount: float
    category: str
    description: str
    frequency: str
    day_of_week: Optional[int] = None
    day_of_month: Optional[int] = None
    start_date: str
    end_date: Optional[str] = None
    is_active: int
    created_at: str
