import { act } from "@testing-library/react";
import { describe, expect, test, vi, beforeEach } from "vitest";

import { useBoardSizing } from "../../main/frontend/hooks/useBoardSizing.js";
import { renderWithStore } from "./test-utils.js";

const observeMock = vi.fn();
const disconnectMock = vi.fn();

class ResizeObserverMock {
    observe = observeMock;
    disconnect = disconnectMock;
}

const HookHarness = ({ isEnabled }: { isEnabled: boolean }) => {
    const boardShellRef = {
        current: isEnabled ? document.createElement("div") : null
    };

    useBoardSizing(boardShellRef, isEnabled);
    return null;
};

describe("useBoardSizing", () => {
    beforeEach(() => {
        observeMock.mockReset();
        disconnectMock.mockReset();
        vi.stubGlobal("ResizeObserver", ResizeObserverMock);
        vi.stubGlobal("matchMedia", vi.fn().mockReturnValue({
            matches: false,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            addListener: vi.fn(),
            removeListener: vi.fn(),
            dispatchEvent: vi.fn()
        }));
    });

    test("attaches sizing observers when the board becomes active after starting disabled", () => {
        const addEventListenerSpy = vi.spyOn(window, "addEventListener");

        const { rerender } = renderWithStore(<HookHarness isEnabled={false} />);

        expect(observeMock).not.toHaveBeenCalled();

        act(() => {
            rerender(<HookHarness isEnabled={true} />);
        });

        expect(observeMock).toHaveBeenCalledTimes(1);
        expect(addEventListenerSpy).toHaveBeenCalledWith("resize", expect.any(Function));
    });
});
