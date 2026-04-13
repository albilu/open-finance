/**
 * selectClickGuard — prevents Radix UI Select click-through to underlying rows.
 *
 * When a Radix Select option (rendered in a portal) is activated via pointerdown,
 * the portal is removed from DOM before the browser fires the subsequent click
 * event. The browser dispatches that click to the element now occupying those
 * screen coordinates — typically a transaction card below the dropdown.
 *
 * This module provides a lightweight, state-free guard:
 *  1. Call `markSelectInteraction()` inside `onValueChange` or `onOpenChange`
 *     of any Select component that sits above the transaction list.
 *  2. Call `wasSelectJustClosed()` at the start of the card's onClick handler
 *     and bail out early if true.
 *
 * The flag self-clears via requestAnimationFrame, which fires after any pending
 * browser click events originating from the same pointer interaction.
 */

let _selectJustClosed = false;

export function markSelectInteraction(): void {
  _selectJustClosed = true;
  requestAnimationFrame(() => {
    _selectJustClosed = false;
  });
}

export function wasSelectJustClosed(): boolean {
  return _selectJustClosed;
}
