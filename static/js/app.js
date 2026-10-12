/* Kanakani browser script. No inline scripts are allowed by the CSP, so everything lives here.
   The server checks every post again, so this file only helps; it is never relied on for safety. */
(function () {
  'use strict';

  // Keep these patterns the same as SlipGuard.java.
  var CHECKS = [
    { re: /[\w.+-]+@[\w-]+\.[\w.]+/, label: 'an email address' },
    { re: /(?:\+?\d[\s-]?){10,}/, label: 'a phone number' },
    { re: /https?:\/\/|www\./i, label: 'a link' },
    { re: /\b[A-Za-z]{2,4}-?\d{4,}\b/, label: 'what looks like an employee ID' }
  ];

  function findSlips(text) {
    var found = [];
    for (var i = 0; i < CHECKS.length; i++) {
      if (CHECKS[i].re.test(text)) { found.push(CHECKS[i].label); }
    }
    return found;
  }

  var draft = document.getElementById('postdraft');
  var hint = document.getElementById('slip');
  if (draft && hint) {
    draft.addEventListener('input', function () {
      var found = findSlips(draft.value);
      hint.textContent = found.length
        ? 'Careful, this has ' + found.join(', ') + '. It could point back to you.'
        : '';
    });
  }

  // Stop double taps from posting twice.
  var forms = document.querySelectorAll('form');
  Array.prototype.forEach.call(forms, function (form) {
    form.addEventListener('submit', function () {
      if (form.dataset.sent === '1') { return; }
      form.dataset.sent = '1';
      var buttons = form.querySelectorAll('button[type="submit"]');
      // Disable after the browser has read the clicked button's value.
      setTimeout(function () {
        Array.prototype.forEach.call(buttons, function (b) { b.disabled = true; });
      }, 0);
    });
  });

  // Restoring a page from the back button must not leave buttons stuck.
  window.addEventListener('pageshow', function (e) {
    if (!e.persisted) { return; }
    Array.prototype.forEach.call(forms, function (f) {
      f.dataset.sent = '0';
      Array.prototype.forEach.call(f.querySelectorAll('button[type="submit"]'), function (b) { b.disabled = false; });
    });
  });
})();
