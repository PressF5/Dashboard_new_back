package org.pressf5.dashboard.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminRoleCode {

    STATISTICS_ADMIN("Администратор основной статистики"),
    PUBLICATIONS_ADMIN("Администратор статистики публикаций"),
    TILDA_ADMIN("Администратор статистики Tilda"),
    DISTANCE_ADMIN("Администратор статистики дистанционного обучения");

    private final String name;
}
