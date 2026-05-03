declare module 'sockjs-client' {
  interface SockJSEvent {
    type: string;
    data?: string;
  }

  class SockJS {
    constructor(url: string, _reserved?: unknown, options?: { protocols?: string | string[]; transports?: string | string[] });
    onopen: ((e: SockJSEvent) => void) | null;
    onmessage: ((e: SockJSEvent) => void) | null;
    onclose: ((e: SockJSEvent) => void) | null;
    send(data: string): void;
    close(): void;
  }

  export default SockJS;
}
