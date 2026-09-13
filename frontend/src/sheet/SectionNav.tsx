import { useEffect, useState } from 'react';
import { sheetSectionId } from './SheetFields';

interface Props {
  sections: { name: string }[];
}

/**
 * A rail of section names down the side of a character sheet. Clicking a name
 * jumps straight to that section; it never scrolls on its own. The highlighted
 * item tracks whichever section is currently in view as the user scrolls.
 */
export function SectionNav({ sections }: Props) {
  const [activeId, setActiveId] = useState(sheetSectionId(sections[0]?.name ?? ''));

  useEffect(() => {
    const elements = sections
      .map((section) => document.getElementById(sheetSectionId(section.name)))
      .filter((el): el is HTMLElement => el !== null);

    if (elements.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) {
          setActiveId(visible[0].target.id);
        }
      },
      { rootMargin: '-100px 0px -70% 0px', threshold: 0 },
    );

    elements.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [sections]);

  function jumpTo(name: string) {
    document.getElementById(sheetSectionId(name))?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  if (sections.length < 2) return null;

  return (
    <nav className="section-nav" aria-label="Jump to sheet section">
      {sections.map((section) => {
        const id = sheetSectionId(section.name);
        return (
          <button
            key={id}
            type="button"
            className={`section-nav-item${activeId === id ? ' active' : ''}`}
            onClick={() => jumpTo(section.name)}
          >
            <span className="section-nav-dot" />
            {section.name}
          </button>
        );
      })}
    </nav>
  );
}
