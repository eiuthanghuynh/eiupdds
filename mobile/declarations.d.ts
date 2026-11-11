/// <reference types="expo/types" />
/// <reference types="nativewind/types" />
/// <reference types="react-native-css/types" />

declare module '*.module.css' {
  const classes: { readonly [key: string]: string };
  export default classes;
}

declare module '*.css' {
  const content: Record<string, string>;
  export default content;
}
