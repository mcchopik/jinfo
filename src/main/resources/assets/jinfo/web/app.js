(function () {
  "use strict";

  var base = location.pathname.replace(/\/+$/, "");
  var textEl = document.getElementById("text");
  var counterEl = document.getElementById("counter");
  var limitsEl = document.getElementById("limits");
  var statusEl = document.getElementById("status");
  var sessionEl = document.getElementById("session");
  var warningsEl = document.getElementById("warnings");
  var tooltipEl = document.getElementById("preview-tooltip");
  var fullEl = document.getElementById("preview-full");
  var saveBtn = document.getElementById("save");

  var COLOR_CODES = ["0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"];
  var COLORS = ["#000000","#0000aa","#00aa00","#00aaaa","#aa0000","#aa00aa","#ffaa00","#aaaaaa",
                "#555555","#5555ff","#55ff55","#55ffff","#ff5555","#ff55ff","#ffff55","#ffffff"];
  var SERVICES = ["youtube","twitch","discord","telegram","tiktok","instagram","x","github",
                  "steam","vk","reddit","kick","boosty","donationalerts","modrinth"];
  var STYLES = [["&l","Ж"],["&o","К"],["&n","Ч"],["&m","З"],["&r","Сброс"]];

  var rev = -1;
  var dirty = false;
  var lastSaved = "";
  var alive = true;
  var previewTimer = null;

  function setStatus(msg, cls) {
    statusEl.textContent = msg || "";
    statusEl.className = "status" + (cls ? " " + cls : "");
  }

  function markDirty() {
    dirty = textEl.value !== lastSaved;
  }

  function insertAtCursor(s) {
    var start = textEl.selectionStart;
    var end = textEl.selectionEnd;
    var value = textEl.value;
    textEl.value = value.slice(0, start) + s + value.slice(end);
    var pos = start + s.length;
    textEl.selectionStart = pos;
    textEl.selectionEnd = pos;
    textEl.focus();
    onInput();
  }

  function buildColors() {
    var container = document.getElementById("colors");
    COLOR_CODES.forEach(function (code, i) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "colbtn mc" + code;
      b.title = "&" + code + " (" + COLORS[i] + ")";
      b.addEventListener("click", function () { insertAtCursor("&" + code); });
      container.appendChild(b);
    });
  }

  function buildStyles() {
    var container = document.getElementById("styles");
    STYLES.forEach(function (pair) {
      var b = document.createElement("button");
      b.type = "button";
      b.textContent = pair[1];
      b.title = pair[0];
      b.addEventListener("click", function () { insertAtCursor(pair[0]); });
      container.appendChild(b);
    });
  }

  function buildServices() {
    var container = document.getElementById("services");
    var avatar = document.createElement("button");
    avatar.type = "button";
    avatar.className = "svc-avatar";
    avatar.textContent = "avatar";
    avatar.title = "{avatar} https://...";
    avatar.addEventListener("click", function () { insertAtCursor("{avatar} "); });
    container.appendChild(avatar);
    SERVICES.forEach(function (key) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "svc-" + key;
      b.textContent = key;
      b.addEventListener("click", function () { insertAtCursor("{" + key + "} "); });
      container.appendChild(b);
    });
  }

  function updateLocalCounter() {
    var chars = textEl.value.length;
    var lines = textEl.value.split("\n").length;
    counterEl.textContent = chars + " / 500 симв., " + lines + " / 10 строк";
    counterEl.className = chars > 500 ? "bad" : "";
  }

  function requestPreview() {
    updateLocalCounter();
    if (!alive) { return; }
    fetch(base + "/preview", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: textEl.value, lang: "ru" })
    }).then(function (r) { return r.json(); }).then(function (data) {
      tooltipEl.innerHTML = data.tooltip || "";
      fullEl.innerHTML = data.full || "";
      var warn = [];
      if (data.overLimits) { warn.push("Превышены лимиты."); }
      if (data.links > 6) { warn.push("Ссылок: " + data.links + " (макс. 6)."); }
      warningsEl.textContent = warn.join(" ");
      limitsEl.textContent = "ссылок: " + (data.links || 0);
    }).catch(function () {
      warningsEl.textContent = "Не удалось получить предпросмотр.";
    });
  }

  function onInput() {
    markDirty();
    updateLocalCounter();
    if (previewTimer) { clearTimeout(previewTimer); }
    previewTimer = setTimeout(requestPreview, 200);
  }

  function loadProfile() {
    return fetch(base + "/profile").then(function (r) { return r.json(); }).then(function (data) {
      if (!data.empty && typeof data.text === "string") {
        textEl.value = data.text;
        lastSaved = data.text;
      }
      if (typeof data.rev === "number") { rev = data.rev; }
      dirty = false;
      requestPreview();
      if (data.verified === false) {
        setStatus("Профиль offline-аккаунта (не подтверждён).", "warn");
      }
    }).catch(function () {
      setStatus("Не удалось загрузить профиль.", "err");
    });
  }

  function save() {
    if (!alive) {
      setStatus("Сессия закрыта — игра не отвечает.", "err");
      return;
    }
    saveBtn.disabled = true;
    setStatus("Сохранение…", "");
    fetch(base + "/save", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: textEl.value, rev: rev })
    }).then(function (r) { return r.json(); }).then(function (data) {
      saveBtn.disabled = false;
      if (data.ok) {
        if (typeof data.rev === "number") { rev = data.rev; }
        lastSaved = textEl.value;
        dirty = false;
        setStatus("Профиль сохранён.", "ok");
        if (data.stale) {
          setStatus("Сохранено. Другая вкладка тоже сохраняла — последняя запись победила.", "warn");
        }
      } else if (data.error === "limits") {
        setStatus("Превышены лимиты: 500 символов / 10 строк / 6 ссылок.", "err");
      } else if (data.rateLimited) {
        setStatus("Слишком часто. Подожди немного.", "warn");
      } else {
        setStatus("Ошибка сохранения: " + (data.error || "неизвестно"), "err");
      }
    }).catch(function () {
      saveBtn.disabled = false;
      setStatus("Ошибка сети. Текст остался в поле — попробуй ещё раз.", "err");
    });
  }

  function setupTabs() {
    var tabs = document.querySelectorAll(".tab");
    tabs.forEach(function (tab) {
      tab.addEventListener("click", function () {
        tabs.forEach(function (t) { t.classList.remove("active"); });
        tab.classList.add("active");
        var name = tab.getAttribute("data-tab");
        document.getElementById("preview-tooltip").classList.toggle("active", name === "tooltip");
        document.getElementById("preview-full").classList.toggle("active", name === "full");
      });
    });
  }

  function ping() {
    if (!alive) { return; }
    fetch(base + "/ping").then(function (r) {
      if (!r.ok) { throw new Error("bad"); }
      sessionEl.textContent = "Соединение активно";
      sessionEl.className = "session ok";
    }).catch(function () {
      alive = false;
      sessionEl.textContent = "Сессия закрыта — игра не отвечает";
      sessionEl.className = "session dead";
      saveBtn.disabled = true;
      setStatus("Сессия закрыта. Запусти /jinfo web снова.", "err");
    });
  }

  textEl.addEventListener("input", onInput);
  document.getElementById("save").addEventListener("click", save);
  document.getElementById("clear").addEventListener("click", function () {
    textEl.value = "";
    onInput();
    setStatus("Текст очищен (не сохранён).", "");
  });
  document.getElementById("reload").addEventListener("click", loadProfile);
  document.getElementById("hexbtn").addEventListener("click", function () {
    var v = document.getElementById("hex").value.trim();
    if (!v.startsWith("#")) { v = "#" + v; }
    if (/^#[0-9a-fA-F]{6}$/.test(v)) {
      insertAtCursor("&#" + v.substring(1).toUpperCase());
    } else {
      setStatus("Неверный hex-цвет. Формат: #RRGGBB", "err");
    }
  });

  window.addEventListener("beforeunload", function (e) {
    if (dirty) {
      e.preventDefault();
      e.returnValue = "";
      return "";
    }
    return undefined;
  });

  buildColors();
  buildStyles();
  buildServices();
  setupTabs();
  updateLocalCounter();
  loadProfile();
  setInterval(ping, 5000);
  ping();
})();