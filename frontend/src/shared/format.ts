const SQUARE_METERS_PER_HECTARE = 10_000
const METERS_PER_KILOMETER = 1000

const wholeNumber = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 })
const twoDecimals = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})
const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'BRL' })

/** Square meters for small areas, hectares from one hectare up: "8,450 m²", "1.22 ha". */
export function formatArea(squareMeters: number): string {
  if (squareMeters < SQUARE_METERS_PER_HECTARE) {
    return `${wholeNumber.format(squareMeters)} m²`
  }
  return `${twoDecimals.format(squareMeters / SQUARE_METERS_PER_HECTARE)} ha`
}

/**
 * Meters below one kilometer, kilometers from there: "850 m", "1.25 km". Rounds first, so 999.6
 * reads "1.00 km" instead of "1,000 m".
 */
export function formatDistance(meters: number): string {
  const rounded = Math.round(meters)
  if (rounded < METERS_PER_KILOMETER) {
    return `${wholeNumber.format(rounded)} m`
  }
  return `${twoDecimals.format(meters / METERS_PER_KILOMETER)} km`
}

/** Price divided by area, or null while either is missing or not positive. */
export function pricePerSquareMeter(
  price: number | undefined,
  areaSquareMeters: number | null,
): number | null {
  if (
    !price ||
    price <= 0 ||
    !Number.isFinite(price) ||
    !areaSquareMeters ||
    areaSquareMeters <= 0
  ) {
    return null
  }
  return price / areaSquareMeters
}

export function formatCurrency(value: number): string {
  return currency.format(value)
}
