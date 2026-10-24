import { describe, expect, it } from 'vitest'
import { drawKeyAction } from './drawKeys'

describe('drawKeyAction', () => {
  const onMap = document.createElement('div')

  it('cancels the sketch on Escape and removes the last vertex on Backspace', () => {
    expect(drawKeyAction({ key: 'Escape', target: onMap })).toBe('abort')
    expect(drawKeyAction({ key: 'Backspace', target: onMap })).toBe('undo')
    expect(drawKeyAction({ key: 'Escape', target: null })).toBe('abort')
  })

  it('ignores other keys', () => {
    expect(drawKeyAction({ key: 'Enter', target: onMap })).toBeNull()
  })

  it.each(['input', 'textarea', 'select'])('ignores keys typed into a %s', (tag) => {
    const field = document.createElement(tag)

    expect(drawKeyAction({ key: 'Backspace', target: field })).toBeNull()
    expect(drawKeyAction({ key: 'Escape', target: field })).toBeNull()
  })

  it('ignores keys typed into editable content', () => {
    const editable = document.createElement('div')
    Object.defineProperty(editable, 'isContentEditable', { value: true })

    expect(drawKeyAction({ key: 'Backspace', target: editable })).toBeNull()
  })
})
