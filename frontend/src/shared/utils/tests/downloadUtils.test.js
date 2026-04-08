import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

vi.mock('../../api/axios', () => ({
  default: { get: vi.fn() },
}));

import api from '../../api/axios';
import { downloadFile, BLOB_RESPONSE_CONFIG } from '../downloadUtils';

describe('downloadUtils', () => {
  let createObjectURLSpy;
  let revokeObjectURLSpy;
  let clickSpy;
  let createElementSpy;
  let fakeLink;

  beforeEach(() => {
    createObjectURLSpy = vi.fn(() => 'blob:fake-url');
    revokeObjectURLSpy = vi.fn();
    clickSpy = vi.fn();
    fakeLink = { href: '', download: '', click: clickSpy };

    URL.createObjectURL = createObjectURLSpy;
    URL.revokeObjectURL = revokeObjectURLSpy;
    createElementSpy = vi
      .spyOn(globalThis.document, 'createElement')
      .mockReturnValue(fakeLink);

    api.get.mockResolvedValue({ data: new Blob(['hello']) });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exposes BLOB_RESPONSE_CONFIG', () => {
    expect(BLOB_RESPONSE_CONFIG).toEqual({ responseType: 'blob' });
  });

  it('fetches the file as a blob and triggers a click on a temporary link', async () => {
    await downloadFile('/api/files/abc', 'invoice.pdf');

    expect(api.get).toHaveBeenCalledWith('/api/files/abc', BLOB_RESPONSE_CONFIG);
    expect(createElementSpy).toHaveBeenCalledWith('a');
    expect(fakeLink.href).toBe('blob:fake-url');
    expect(fakeLink.download).toBe('invoice.pdf');
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:fake-url');
  });

  it('propagates errors from the API call', async () => {
    api.get.mockRejectedValue(new Error('network down'));
    await expect(downloadFile('/x', 'y.pdf')).rejects.toThrow('network down');
    expect(clickSpy).not.toHaveBeenCalled();
  });
});
