import { trailblaze } from "@trailblaze/scripting";

export interface TapWithAdbInput {
  /** The text of the element, as textRegex in a selector. */
  textRegex: string;
}

/**
 * Tap the one element with this text through adb. Android's folder picker ignores the taps of
 * tapOnElementBySelector, but takes these.
 */
export const voice_tapWithAdb = trailblaze.tool<TapWithAdbInput>(
  { supportedPlatforms: ["android"] },
  async (input, ctx) => {
    const [matches] = await ctx.tools.findSelectorMatches({
      selectors: [{ androidAccessibility: { textRegex: input.textRegex } }],
      timeoutMs: 15_000,
    });
    const bounds = matches.length === 1 ? matches[0].bounds : undefined;
    if (!bounds) {
      throw new Error(`Found ${matches.length} elements with the text ${input.textRegex}, not one.`);
    }
    const x = Math.floor((bounds.left + bounds.right) / 2);
    const y = Math.floor((bounds.top + bounds.bottom) / 2);
    await ctx.tools.android_adbShell({ command: ["input", "tap", String(x), String(y)] });
    return `Tapped ${input.textRegex} at ${x}, ${y}.`;
  },
);
