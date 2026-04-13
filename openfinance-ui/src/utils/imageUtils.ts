/**
 * Utility functions for optimized image loading.
 * Requirement REQ-3.1: Frontend performance - image optimization.
 */

/**
 * Returns an object with loading="lazy" attribute for img elements.
 * Use this helper to ensure consistent lazy loading across the app.
 */
export const lazyImageProps = {
  loading: 'lazy' as const,
  decoding: 'async' as const,
};

/**
 * Generates srcSet for responsive images.
 * @param src - Base image URL
 * @param widths - Array of width breakpoints
 * @returns srcset string
 */
export const generateSrcSet = (src: string, widths: number[] = [320, 640, 960, 1280]): string => {
  return widths.map(w => `${src}?w=${w} ${w}w`).join(', ');
};
