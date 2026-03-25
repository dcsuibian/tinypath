package com.dcsuibian.tinypath;

sealed interface Step permits FieldStep, IndexStep, FilterStep {
}
