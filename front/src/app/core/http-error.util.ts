export function extractErrorMessage(error: unknown): string {
  if (typeof error !== 'object' || error === null || !('error' in error)) {
    return '';
  }

  const body = (error as { error?: unknown }).error;
  if (typeof body === 'object' && body !== null && 'message' in body) {
    return String((body as { message?: unknown }).message);
  }
  if (typeof body === 'string') {
    return body;
  }
  return '';
}
