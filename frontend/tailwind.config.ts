import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: '#1e2433',
        'surface-2': '#252d3d',
        'surface-3': '#2d3748',
        border: '#2d3748',
        accent: '#6c8fff',
        'accent-hover': '#5a7aff',
      },
      animation: {
        'pulse-dot': 'pulse 1.4s ease-in-out infinite',
      },
    },
  },
  plugins: [],
} satisfies Config
