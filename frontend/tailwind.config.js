/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: '#F8FAFC',
        'surface-card': '#FFFFFF',
        'surface-container': '#F1F5F9',
        'surface-container-low': '#F8FAFC',
        primary: '#4F46E5', // Soft indigo / blue
        'primary-hover': '#4338CA',
        'primary-light': '#EEF2FF',
        'on-primary': '#ffffff',
        secondary: '#0F172A',
        tertiary: '#F59E0B', // Soft amber for udhaar
        'on-surface': '#0F172A',
        'on-surface-variant': '#64748B',
        outline: '#E2E8F0',
        error: '#EF4444',
        'error-bg': '#FEF2F2',
        'error-text': '#991B1B',
        success: '#10B981',
        'success-bg': '#ECFDF5',
        'success-text': '#065F46',
        warning: '#F59E0B',
        'warning-bg': '#FFFBEB',
        'warning-text': '#92400E',
        'jama-bg': '#ECFDF5',
        'jama-text': '#047857',
        'udhaar-bg': '#FFFBEB',
        'udhaar-text': '#B45309'
      },
      fontFamily: {
        display: ['Outfit', 'Inter', 'sans-serif'],
        body: ['Inter', 'sans-serif']
      },
      borderRadius: {
        card: '0.875rem',
        action: '0.625rem'
      },
      boxShadow: {
        card: '0 1px 3px 0 rgb(0 0 0 / 0.05), 0 1px 2px -1px rgb(0 0 0 / 0.05)',
        'card-hover': '0 4px 12px -2px rgb(0 0 0 / 0.08), 0 2px 4px -2px rgb(0 0 0 / 0.04)',
        modal: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)'
      },
      keyframes: {
        pulseWave: {
          '0%, 100%': { transform: 'scale(1)', opacity: '0.8' },
          '50%': { transform: 'scale(1.15)', opacity: '0.3' }
        },
        slideInRight: {
          '0%': { transform: 'translateX(100%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' }
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        scaleUp: {
          '0%': { transform: 'scale(0.95)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        }
      },
      animation: {
        pulseWave: 'pulseWave 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        slideInRight: 'slideInRight 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards',
        fadeIn: 'fadeIn 0.2s ease-out forwards',
        scaleUp: 'scaleUp 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards'
      }
    }
  },
  plugins: []
}

