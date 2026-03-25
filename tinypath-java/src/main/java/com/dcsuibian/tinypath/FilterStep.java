package com.dcsuibian.tinypath;

import java.util.List;

record FilterStep(List<Condition> conditions) implements Step {
}
