# TPI_VS

плагин для фолии, делал для vnlla survival

## фичи

- таб с префиксами и автообновлением, заголовок настраивается
- афк система — ручная через /afk и авто по таймауту, мобы не трогают афкшников
- админ просмотр инвентаря и эндера
- гуи для настройки префикса с цветами градиентами и форматированием

## надо

- фолия 1.21.11+
- ресурспак vnlla-resourcepack (без него таб кривой будут квадраты)

## команды

| команда | что делает | право |
|---------|-----------|-------|
| /afk | уйти в афк | tpi_vs.afk |
| /invsee <ник> | посмотреть инвентарь | tpi_vs.admin |
| /endersee <ник> | посмотреть эндер | tpi_vs.admin |
| /prefix | настроить префикс | tpi_vs.prefix |
| /prefix set <ник> | настроить чужой префикс | tpi_vs.admin |
| /prefix reset <ник> | сбросить префикс | tpi_vs.admin |
| /prefix lock <ник> | заблокировать смену | tpi_vs.admin |
| /prefix unlock <ник> | разблокировать | tpi_vs.admin |
| /tpi_vs reload | перезагрузить конфиг | tpi_vs.admin |

## конфиг

```yaml
tab:
  update-interval-ticks: 60
  server-name: "VNLLA.RU"
  server-sub-name: "survival"

afk:
  timeout-seconds: 300
  hostile-mob-radius: 10
  mob-protection: true

prefix:
  max-length: 5

admin:
  permission: "tpi_vs.admin"
```

## собрать

```
mvn clean package
```

jar будет в target/TPI_VS.jar

## про ресурспак

для выравнивания ников используются невидимые символы, они должны быть в ресурспаке иначе будут квадраты и всё съедет. ресурспак обязателен.

### как установить

ресурспак лежит на гитхабе: https://github.com/Calladius/vnlla-resourcepack/releases

впиши в `server.properties`:
```properties
require-resource-pack=true
resource-pack=https://github.com/Calladius/vnlla-resourcepack/releases/download/v1.1/vnlla-resourcepack.zip
resource-pack-sha1=5a6e723e2fbac1c8c49e4b340088cf63257b00e7
resource-pack-prompt={"text":"Для таба нужен ресурспак","color":"gold"}
```

перезапусти сервер. при входе клиент скачает пак и попросит его включить

если клиент пишет что пак несовместим — проверь `pack_format` в `pack.mcmeta`, для 1.21.11 должно быть 75

автор: calladius
