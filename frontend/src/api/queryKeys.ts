/** Queries whose data belongs to the logged in user; logout drops everything under this key. */
export const MY_DATA_KEY = ['me'] as const

export const MY_LOTS_KEY = [...MY_DATA_KEY, 'land-lots'] as const
