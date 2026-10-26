const DDD_DIGITS = 2
const MAX_DIGITS = 11
const BRAZIL_CODE = '55'

/**
 * Brazilian phone as maskPhone writes it: DDD with two digits from 1 to 9 (no DDD has a zero),
 * then a landline (8 digits starting with 2 to 5) or a mobile (9 digits starting with 9). Mirrors
 * ValidationPatterns.BRAZILIAN_PHONE of the backend.
 */
const BRAZILIAN_PHONE = /^\([1-9]{2}\) (?:[2-5]\d{3}|9\d{4})-\d{4}$/

export function isBrazilianPhone(text: string): boolean {
  return BRAZILIAN_PHONE.test(text)
}

/**
 * Formats the digits typed so far: "(84", "(84) 9999", "(84) 3333-4444", "(84) 99999-8888". Any
 * other character is dropped. A pasted "+55 84 99999-8888" loses its country code; only with the
 * plus sign, because 55 is also a DDD (Rio Grande do Sul).
 */
export function maskPhone(text: string): string {
  let digits = text.replace(/\D/g, '')
  if (text.trim().startsWith(`+${BRAZIL_CODE}`)) {
    digits = digits.slice(BRAZIL_CODE.length)
  }
  digits = digits.slice(0, MAX_DIGITS)
  if (digits.length === 0) {
    return ''
  }
  const ddd = digits.slice(0, DDD_DIGITS)
  const number = digits.slice(DDD_DIGITS)
  if (number.length === 0) {
    return `(${ddd}`
  }
  if (number.length <= 4) {
    return `(${ddd}) ${number}`
  }
  // Landline layout (4-4) while typing; the ninth digit makes it a mobile (5-4)
  const split = number.length === 9 ? 5 : 4
  return `(${ddd}) ${number.slice(0, split)}-${number.slice(split)}`
}

/**
 * tel: link of a stored phone. Spaces and parentheses are not allowed in a tel URI (RFC 3966), so
 * only the digits are kept, with the country code: "(84) 99999-8888" becomes "+5584999998888". A
 * phone already starting with + keeps its own country code.
 */
export function telHref(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  return phone.trim().startsWith('+') ? `tel:+${digits}` : `tel:+${BRAZIL_CODE}${digits}`
}
