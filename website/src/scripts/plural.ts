export const plural = (value: number, word: string) => `${value} ${value === 1 ? word : `${word}s`}`
