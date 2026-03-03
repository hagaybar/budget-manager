"""Internal data representations and helper constants.

This module can hold enums, constants, or internal model helpers
that are not directly exposed as API schemas.
"""

from enum import Enum


class TransactionType(str, Enum):
    """Valid transaction types."""
    INCOME = "income"
    EXPENSE = "expense"
