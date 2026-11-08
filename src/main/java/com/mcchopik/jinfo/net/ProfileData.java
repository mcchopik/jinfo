package com.mcchopik.jinfo.net;

/**
 * Профиль, полученный с Worker.
 *
 * @param uuid     UUID владельца
 * @param data     исходный текст профиля (с плейсхолдерами)
 * @param updated  метка времени последнего изменения (секунды)
 * @param verified true для лицензионных (UUID v4), false для offline (UUID v3)
 */
public record ProfileData(String uuid, String data, long updated, boolean verified) {
}