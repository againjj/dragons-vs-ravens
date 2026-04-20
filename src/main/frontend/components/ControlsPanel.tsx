import { useAppSelector } from "../app/hooks.js";
import {
    selectCanViewerAct,
    selectCanViewerUndo,
    selectCurrentRuleConfiguration,
    selectIsFinishedGame,
    selectIsSubmitting,
    selectSnapshot
} from "../features/game/gameSelectors.js";
import type { RuleConfigurationSummary, Side } from "../game-types.js";

interface GameSetupControlsProps {
    availableRuleConfigurations: RuleConfigurationSummary[];
    selectedRuleConfigurationId: string;
    selectedStartingSide: Side;
    selectedBoardSize: number;
    isDisabled: boolean;
    onSelectRuleConfiguration: (ruleConfigurationId: string) => void;
    onSelectStartingSide: (side: Side) => void;
    onSelectBoardSize: (boardSize: number) => void;
    onStartGame?: () => void | Promise<void>;
}

export const GameSetupControls = ({
    availableRuleConfigurations,
    selectedRuleConfigurationId,
    selectedStartingSide,
    selectedBoardSize,
    isDisabled,
    onSelectRuleConfiguration,
    onSelectStartingSide,
    onSelectBoardSize,
    onStartGame
}: GameSetupControlsProps) => {
    const showSizeAndSideSelectors = selectedRuleConfigurationId === "free-play";
    const startButtonDisabled = isDisabled || !onStartGame;

    return (
        <>
            <div className="control-row">
                <label className="control-label" htmlFor="rule-configuration-select">
                    Play Style
                </label>
                <div className="select-shell">
                    <select
                        id="rule-configuration-select"
                        value={selectedRuleConfigurationId}
                        disabled={isDisabled}
                        onChange={(event) => {
                            onSelectRuleConfiguration(event.target.value);
                        }}
                    >
                        {availableRuleConfigurations.map((ruleConfiguration) => (
                            <option key={ruleConfiguration.id} value={ruleConfiguration.id}>
                                {ruleConfiguration.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>
            {showSizeAndSideSelectors ? (
                <>
                    <div className="control-row">
                        <label className="control-label" htmlFor="board-size-select">
                            Board Size
                        </label>
                        <div className="select-shell">
                            <select
                                id="board-size-select"
                                value={String(selectedBoardSize)}
                                disabled={isDisabled}
                                onChange={(event) => {
                                    onSelectBoardSize(Number.parseInt(event.target.value, 10));
                                }}
                            >
                                {Array.from({ length: 24 }, (_, index) => index + 3).map((boardSize) => (
                                    <option key={boardSize} value={boardSize}>
                                        {boardSize}x{boardSize}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <div className="control-row">
                        <label className="control-label" htmlFor="starting-side-select">
                            Starting Side
                        </label>
                        <div className="select-shell">
                            <select
                                id="starting-side-select"
                                value={selectedStartingSide}
                                disabled={isDisabled}
                                onChange={(event) => {
                                    onSelectStartingSide(event.target.value as Side);
                                }}
                            >
                                <option value="dragons">Dragons</option>
                                <option value="ravens">Ravens</option>
                            </select>
                        </div>
                    </div>
                </>
            ) : null}
            <button
                id="start-button"
                type="button"
                disabled={startButtonDisabled}
                onClick={() => {
                    onStartGame?.();
                }}
            >
                Start Game
            </button>
        </>
    );
};

interface ControlsPanelProps {
    onEndGame: () => void;
    onUndo: () => void;
    onSkipCapture: () => void;
}

export const ControlsPanel = ({
    onEndGame,
    onUndo,
    onSkipCapture
}: ControlsPanelProps) => {
    const snapshot = useAppSelector(selectSnapshot);
    const canViewerAct = useAppSelector(selectCanViewerAct);
    const canViewerUndo = useAppSelector(selectCanViewerUndo);
    const isSubmitting = useAppSelector(selectIsSubmitting);
    const isFinishedGame = useAppSelector(selectIsFinishedGame);
    const currentRuleConfiguration = useAppSelector(selectCurrentRuleConfiguration);
    const disabled = !snapshot || isSubmitting || !canViewerAct;
    const phase = snapshot?.phase;
    const isActivePlay = phase === "move" || phase === "capture";
    const canSkipCapture = phase === "capture" && currentRuleConfiguration?.hasManualCapture;
    const canManualEndGame = isActivePlay && currentRuleConfiguration?.hasManualEndGame;
    const showUndo = (isActivePlay || isFinishedGame) && !!snapshot;
    const undoButton = (
        <button
            id="undo-button"
            type="button"
            disabled={isSubmitting || !canViewerUndo}
            onClick={onUndo}
        >
            Undo
        </button>
    );

    return (
        <div className="controls controls-sidebar">
            {isActivePlay ? (
                <>
                    {currentRuleConfiguration?.hasManualCapture ? (
                        <button
                            id="capture-skip-button"
                            type="button"
                            disabled={disabled || !canSkipCapture}
                            onClick={onSkipCapture}
                        >
                            Skip Capture
                        </button>
                    ) : null}
                    {showUndo ? undoButton : null}
                    {canManualEndGame ? (
                        <button
                            id="end-game-button"
                            type="button"
                            disabled={disabled}
                            onClick={onEndGame}
                        >
                            End Game
                        </button>
                    ) : null}
                </>
            ) : null}
            {showUndo && !isActivePlay ? undoButton : null}
        </div>
    );
};
