# Caching mit ehcache #

Da es sich hier um eine vorwiegend lesende Anwendung handelt, bietet sich Caching an.
  * Vorerst werden nur die Slots gecached.
  * Caching muß in BF\_PLConfig.xml eingeschaltet werden.
  * Default-Werte stehen in ehcache.xml
  * Max 10000 Einträge
  * Max 10 Minuten

Ein ObjektCache macht bei einer Demo viel Eindruck, dürfte aber in der Praxis