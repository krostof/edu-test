package com.edutest.persistance.entity.test;

/**
 * Types of suspicious behavior detected during a test attempt.
 *
 * Used for soft warnings — recorded for the teacher's review, not auto-enforced.
 * Students may have legitimate reasons (alt-tab to check time, paste from clipboard
 * containing pre-written notes), so the teacher decides what to do with each.
 */
public enum IncidentTypeEnum {
    /** Browser tab became hidden (alt-tab, switched to another tab). */
    TAB_HIDDEN,
    /** Browser window lost focus (clicked outside the browser). */
    WINDOW_BLUR,
    /** Large paste detected in answer/code field — possibly external content. */
    LARGE_PASTE,
    /** DevTools shortcut detected (F12, Ctrl+Shift+I, Ctrl+Shift+J). */
    DEVTOOLS_OPEN,
    /** Student exited fullscreen mode (if test was started fullscreen). */
    FULLSCREEN_EXIT,
    /** Right-click attempt (might be trying to copy/inspect). */
    CONTEXT_MENU
}
