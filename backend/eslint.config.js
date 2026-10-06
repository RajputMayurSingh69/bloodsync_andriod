import js from "@eslint/js";
import globals from "globals";

export default [
  js.configs.recommended,
  {
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "commonjs",
      globals: {
        ...globals.node,
      },
    },
    rules: {
      // Catch undefined variables/methods — would have flagged .document() immediately
      "no-undef": "error",
      // Warn on unused variables to keep code clean
      "no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }],
      // Prevent using console.log in production paths (use loggerBotService instead)
      "no-console": "off",
      // Enforce === over == to prevent type coercion bugs
      "eqeqeq": ["error", "always"],
      // Prevent unreachable code after return/throw
      "no-unreachable": "error",
    },
  },
];
