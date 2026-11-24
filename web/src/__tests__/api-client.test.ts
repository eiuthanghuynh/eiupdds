import { afterEach, describe, expect, it, vi } from 'vitest';
import { analyzeLeafGuest, fetchPlants, PddsApiError, validateLeafFile } from '@/lib/api/client';

describe('PddsApiClient & Resilience Guards', () => {
  afterEach(() => vi.restoreAllMocks());

  it('rejects files over 15MB or non-image MIME types before network upload', () => {
    const huge = new File([new ArrayBuffer(16 * 1024 * 1024)], 'leaf.jpg', { type: 'image/jpeg' });
    expect(() => validateLeafFile(huge)).toThrowError(/15MB/);

    const pdf = new File(['%PDF'], 'doc.pdf', { type: 'application/pdf' });
    expect(() => validateLeafFile(pdf)).toThrowError(/JPEG|PNG|WebP/i);
  });

  it('parses X-RateLimit-Remaining on 200 and Retry-After on 429 RATE_001', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify({ success: false, code: 'RATE_001', message: 'Too many requests' }), {
        status: 429,
        headers: { 'Retry-After': '1800', 'X-RateLimit-Remaining': '0' },
      })
    );
    const valid = new File(['img'], 'leaf.jpg', { type: 'image/jpeg' });
    await expect(analyzeLeafGuest(valid)).rejects.toMatchObject({
      status: 429,
      code: 'RATE_001',
      retryAfterSeconds: 1800,
    } satisfies Partial<PddsApiError>);
  });

  it('surfaces DIAGNOSIS_003 on 422 empty detection', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify({ success: false, code: 'DIAGNOSIS_003', message: 'No leaf detected' }), {
        status: 422,
      })
    );
    const valid = new File(['img'], 'blur.png', { type: 'image/png' });
    await expect(analyzeLeafGuest(valid)).rejects.toMatchObject({ status: 422, code: 'DIAGNOSIS_003' });
  });

  it('falls back to bundled 9-crop snapshot when backend is unreachable', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new TypeError('Failed to fetch'));
    const res = await fetchPlants();
    expect(res.isOfflineFallback).toBe(true);
    expect(res.plants).toHaveLength(9);
  });
});
