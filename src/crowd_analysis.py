"""
Crowd Density Classification for Customer-Aware AMR.
Classifies the surrounding area into discrete density categories.
"""

from enum import Enum

class CrowdDensity(str, Enum):
    EMPTY = "EMPTY"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"

def analyze_crowd_density(people_count: int) -> str:
    """
    Returns crowd density string (UPPERCASE for consistent comparison).
    Backward-compatible with original function while guaranteeing uppercase standard.
    """
    if people_count <= 0:
        return CrowdDensity.EMPTY.value
    elif people_count <= 2:
        return CrowdDensity.LOW.value
    elif people_count <= 4:
        return CrowdDensity.MEDIUM.value
    else:
        return CrowdDensity.HIGH.value