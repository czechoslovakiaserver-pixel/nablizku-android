# Nablízku pro Android

Nativní testovací obal současné aplikace Nablízku s podporou SMS ochrany.

## Co projekt umí

- zobrazí aktuální Nablízku jako samostatnou Android aplikaci;
- požádá uživatele o nastavení Nablízku jako výchozí SMS aplikace;
- přijímá nové SMS a upozorní na rizikový text;
- obsahuje nativní přehled SMS z telefonu;
- používá jednoduchá lokální pravidla – hesla, PIN, žádosti o peníze, odkazy, spěch a vydávání se za rodinu;
- data SMS neopouštějí telefon.

## Sestavení bez Android Studia

Projekt obsahuje workflow `.github/workflows/android-apk.yml`. Po nahrání projektu na GitHub se v Actions vytvoří `app-debug.apk`, který lze stáhnout a nainstalovat na Android.

## Bezpečnost

Android dovolí přístup ke všem SMS pouze aplikaci, kterou uživatel vědomě nastaví jako výchozí SMS aplikaci. Během prototypu neposílejte texty zpráv na žádný server.
