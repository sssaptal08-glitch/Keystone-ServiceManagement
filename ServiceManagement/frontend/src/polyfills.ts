const globalScope = globalThis as typeof globalThis & { global?: typeof globalThis };

if (!globalScope.global) {
  globalScope.global = globalThis;
}

export {};
