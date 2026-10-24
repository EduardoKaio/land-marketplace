export type DrawKeyAction = 'abort' | 'undo'

const EDITABLE_TAGS = new Set(['INPUT', 'TEXTAREA', 'SELECT'])

/**
 * What a key does while drawing: Escape cancels the sketch and Backspace removes the last
 * vertex. Keys typed into a form field are ignored, otherwise deleting a letter of the title
 * would delete a vertex.
 */
export function drawKeyAction(event: Pick<KeyboardEvent, 'key' | 'target'>): DrawKeyAction | null {
  if (isEditable(event.target)) {
    return null
  }
  if (event.key === 'Escape') {
    return 'abort'
  }
  if (event.key === 'Backspace') {
    return 'undo'
  }
  return null
}

function isEditable(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLElement && (target.isContentEditable || EDITABLE_TAGS.has(target.tagName))
  )
}
