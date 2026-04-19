import type { RuleDescriptionSection } from "../game-types.js";

interface RulesPanelProps {
    title?: string;
    sections: RuleDescriptionSection[];
    className?: string;
}

export const RulesPanel = ({ title = "Rules", sections, className = "" }: RulesPanelProps) => (
    <section className={`legend${className ? ` ${className}` : ""}`}>
        <h2>{title}</h2>
        {sections.map((section, index) => (
            <div key={`${section.heading ?? "section"}-${index}`} className="legend-section">
                {section.heading ? <h3>{section.heading}</h3> : null}
                {section.paragraphs.map((paragraph) => (
                    <p key={paragraph}>{paragraph}</p>
                ))}
            </div>
        ))}
    </section>
);
