export const OPEN_APP_SHEET_EVENT = 'eiu-pdds:open-app-sheet';

export function getMobileAppDownloadUrl(): string | null {
  const raw = process.env.NEXT_PUBLIC_MOBILE_APP_URL?.trim();
  if (!raw) {
    return null;
  }
  if (/^(https?:\/\/|\/)/i.test(raw) && !/^\/\//.test(raw)) {
    return raw;
  }
  return null;
}

export function openMobileAppDrawer(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new CustomEvent(OPEN_APP_SHEET_EVENT));
}
