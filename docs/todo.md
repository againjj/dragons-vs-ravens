# Todo

This file is the canonical list of planned work that is not being implemented immediately. Keep each item issue-tracker-like, link any backing plan files, and remove completed items plus obsolete backing plan files when the work is done.

## Add a play log to Lunar Base

- **goal**: The play log should allow the user or developer to reconstruct the game.
- **Notes**: The play log should have all information that it can, but private details should only be revealed to that particular player. This means that a person taking a seat will need the game log updated. I expect a game log entry will be sent on every server response, but on initial load or seta taking, the log will need to be sent entire.

## Agents Should Hit Discard After Resolution

- **Goal**: When playing an agent, it needs to not be in the discard until after it fully completes. This means that it is not included in shuffles of the discard pile.
- **Notes**: The only time this matters is when the stock empties. At that time, if an agent is in the middle of being resolved, and a card needs to be drawn, then the agent should not be included in the shuffle, but right now it is.

## Add an Auto-Action to Lunar Base

-- **Goal**: The user should be able to enable a mode so that forced actions are automatically taken, so tha user doesn't need to use any effort.
-- **Notes**: In the action abr, there needs to be a right-justified checkbox that says "Perform required actions automatically". The checkbox is initially unchecked and is cleared whenever a seat is relinquished. If it is checked, the back end should perform the auto-action specified. Auto-action definitions:
Choose One: If there is only one sub-action, the auto-action is to perform that action.
Build: If there is no legal module play, the auto-action is to skip remaining builds of this action.
Draw: The auto-action is to draw N cards.
Draft: If the number to take is zero, the auto-action is to do nothing. If the number to take is greater than or equal to the number in the supply, the auto-action is to take them all. Be wary of taking influences int he right order.
Resell: If the number to move is zero, the auto-action is to do nothing. If the number to move is greater than or equal to the number of non-influences in the supply, the auto-action is to resell them all.
Flip station: (1) If N is self, auto-action: the station is flipped. (2) If N is a number and is zero or equal to the number of players, auto-action: flip none or all of them, as appropriate.
Flip station to: Auto-action: the station is flipped or not as appropriate.
Discard: If the number to discard is zero, the auto-action is to do nothing. If the number to discard is greater than or equal to the number of cards in hand, the auto-action is to discard them all, unless there are "on discard" actions that can cause a change.
Steal credits: If the amount to steal is zero, the auto-action is to do nothing. If there is only one opponent, the auto-action is to steal from that opponent.
Steal a Module: If there is no module that can be stolen or any stealable module cannot be legally placed on your base, the auto-action is to do nothing. If there is only one legal module to steal and only one legal placement, then the auto-action is to steal that module and place it in that location.
Choose an opponent: If there is only one opponent, the auto-action is to choose that one.
Steal Credits: If the forbid stealing credits effect is present, the auto-action becomes do nothing.

## Update the Gin Rummy game scoring

- **Goal:** The game scoring must be clear to the user, and include all bonuses, listed correctly.
- **Notes:** It should list for each player the total hand score, the various itemized bonuses, and the game score.

## Fix the handling of "Server Unavailable"

- **Goal:** The "Server Unavailable" dialog should only appear when there is a real problem that the user needs to know about.
- **Why:** Triggering too often annoys the user for no purpose.
- **Notes:** It seems to appear on flakey connections. An auto-reconnect after a pause seems reasonable, and only if that fails, should there be a notice.

## Prepare Ravens And Dragons For External Game Repos

- **Goal:** Make `ravens-and-dragons` buildable and testable as an independent Gradle project and keep the app's included-game list declarative.
- **Why:** A game module should be able to stay top-level locally or move to another repository later without changing the service boundary.
- **Notes:** Decide later between composite builds, published artifacts, or source checkouts for external games.
- **References:** [docs/multi-game-service-structure-plan.md](/Users/jrayazian/code/ayazian-games/docs/multi-game-service-structure-plan.md).

## Fix Board Edge And Square Sizing

- **Goal:** Review board layout sizing so the board edges and playable squares align cleanly at supported viewport sizes, including fullscreen.
- **Why:** Board geometry problems are highly visible during play and can make interactions feel imprecise.
- **Notes:** Preserve the current board visual language and responsive behavior while tightening the geometry.

## Add Game Skins

- **Goal:** Allow alternate visual skins/themes for the game board and pieces without changing rules or core play flow.
- **Notes:** Treat this as a UI feature unless a future skin requires rules-specific metadata. Preserve existing gameplay behavior.

## Use Michelle As A Search Evaluator

- **Goal:** Add a shallow alpha-beta search wrapper that uses the existing Michelle artifact scorer as the leaf evaluator.
- **Why:** Search would help Michelle handle short tactics, forced replies, and traps without discarding the existing artifact format or training pipeline.
- **Notes:** Keep the existing immediate-win shortcut, use canonical Kotlin legal-move generation, and preserve deterministic tie-breaking.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Add Stronger And More Diverse Michelle Fitness Pressure

- **Goal:** Add baseline pressure during generation or alternate generations between candidate-only and candidate-vs-baseline leagues.
- **Why:** Candidate-only leagues can produce bots that mostly exploit the current population instead of improving against stronger external opponents.
- **Notes:** Useful baselines include the incumbent Michelle, historical Michelle artifacts, `Maxine`, `Alphie`, and supervised seed artifacts with different data settings.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Improve Michelle Fitness Beyond Win Loss Draw

- **Goal:** Add secondary shaped signals to evolution scoring, such as faster wins, slower losses, material at draws, gold progress, containment, threat reduction, and mobility while ahead.
- **Why:** Richer fitness signals can distinguish close candidates when many games draw or produce similar match results.
- **Notes:** Keep actual game results primary so candidates do not learn to optimize good-looking losses.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Teach Michelle From Outcomes

- **Goal:** Add outcome-based self-play examples where moves receive value from the eventual result, possibly discounted by ply distance.
- **Why:** Expert imitation is a useful seed, but outcome learning gives Michelle a path to discover strategies that are not just copies of search-bot choices.
- **Notes:** Keep expert imitation for initial seeds, then mix in value-derived ranking examples or a value-oriented artifact.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Support A Richer Michelle Model Shape

- **Goal:** Add interaction features to the current linear model or eventually support a tiny multilayer perceptron artifact.
- **Why:** Some board concepts are conditional, and a purely linear ranker may not capture those interactions well.
- **Notes:** Prefer explicit interaction features first because they are easier to inspect and evolve.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Specialize Michelle By Game Stage

- **Goal:** Add early, middle, and late-game specialization through separate weights, separate heads, or explicit phase/context features.
- **Why:** The same board fact can mean different things in openings, containment fights, and nearly-terminal escapes.
- **Notes:** Build on schema 5's existing side-specific dragon and raven vectors.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Improve Michelle Opening Diversity And Evaluation Reliability

- **Goal:** Use fixed opening suites and repeated seed schedules so candidate evaluations cover comparable positions from both seats.
- **Why:** More reliable promotion tests reduce lucky promotions and make reports easier to compare between runs.
- **Notes:** This pairs well with stronger reports and shaped fitness.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Use Rating-Based Michelle Survivor Selection

- **Goal:** Evaluate survivors with an Elo or TrueSkill-style rating instead of only raw round-robin scores.
- **Why:** Ratings can better handle noisy match results and uneven confidence once candidates, baselines, incumbents, and historical artifacts all appear in evaluation.
- **Notes:** Reports should still explain why each candidate survived or promoted.
- **References:** [docs/machine-trained-bot-improvements.md](/Users/jrayazian/code/ayazian-games/docs/machine-trained-bot-improvements.md).

## Change Package Path

- **Goal:** Rename the Kotlin package path to match the intended long-term project or module structure.
- **Why:** The current package path uses a top-level domain that I do not own.
- **Notes:** Coordinate this with the multi-game structure plan if the package rename is part of the same modularization work.
