// Approximations of the MaterialShapes the app uses for its stickers and the aurora's sparkles.
type ShapeKind = 'cookie' | 'clover' | 'sunny' | 'flower' | 'burst'

const SHAPES: Record<ShapeKind, { lobes: number; depth: number; sharpness: number }> = {
  cookie: { lobes: 9, depth: 0.12, sharpness: 0.6 },
  clover: { lobes: 4, depth: 0.4, sharpness: 0.5 },
  sunny: { lobes: 8, depth: 0.08, sharpness: 1 },
  flower: { lobes: 8, depth: 0.22, sharpness: 0.6 },
  burst: { lobes: 10, depth: 0.16, sharpness: 1.6 },
}

export interface Shape {
  kind: ShapeKind
  x: number
  y: number
  r: number
  rotate?: number
  color?: 1 | 2
}

export interface Star {
  x: number
  y: number
  r: number
  opacity?: number
}

function shapePath(kind: ShapeKind, r: number): string {
  const { lobes, depth, sharpness } = SHAPES[kind]
  const steps = 720
  const points: string[] = []
  for (let i = 0; i < steps; i++) {
    const theta = (i / steps) * Math.PI * 2
    const bump = Math.pow((1 + Math.cos(lobes * theta)) / 2, sharpness)
    const radius = r * (1 - depth * (1 - bump))
    points.push(`${(Math.cos(theta) * radius).toFixed(2)},${(Math.sin(theta) * radius).toFixed(2)}`)
  }
  return `M${points.join('L')}Z`
}

function starPath(r: number): string {
  const c = r * 0.14
  return `M0,${-r}Q${c},${-c} ${r},0Q${c},${c} 0,${r}Q${-c},${c} ${-r},0Q${-c},${-c} 0,${-r}Z`
}

function svg(x: number, y: number, r: number, inner: string): string {
  const size = r * 2
  return `<svg width="${size}" height="${size}" viewBox="${-r} ${-r} ${size} ${size}" style="left:${x - r}px;top:${y - r}px">${inner}</svg>`
}

export function decorate(container: Element, shapes: Shape[], stars: Star[]): void {
  const shapeSvgs = shapes.map((s) =>
    svg(
      s.x,
      s.y,
      s.r,
      `<path class="shape-${s.color ?? 1}" d="${shapePath(s.kind, s.r)}" transform="rotate(${s.rotate ?? 0})"/>`,
    ),
  )
  const starSvgs = stars.map((s) =>
    svg(s.x, s.y, s.r, `<path class="star" d="${starPath(s.r)}" opacity="${s.opacity ?? 0.7}"/>`),
  )
  container.innerHTML = [...shapeSvgs, ...starSvgs].join('')
}
