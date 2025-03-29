import React from 'react';
import { Highlight, themes } from 'prism-react-renderer';

interface CodeBlockProps {
    code: string;
    language: string;
}

const CodeBlock: React.FC<CodeBlockProps> = ({ code, language }) => {
    const formatXml = (xml: string): string => {
        if (language === 'xml' || language === 'html') {
            const lines = xml.split('\n');
            const result: string[] = [];
            let indentLevel = 0;

            lines.forEach(line => {
                const trimmed = line.trim();
                if (!trimmed) return;

                if (trimmed.startsWith('</')) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }

                if (trimmed.length > 0) {
                    result.push('    '.repeat(indentLevel) + trimmed);
                }
                if (trimmed.includes('<') && !trimmed.includes('</') && !trimmed.endsWith('/>')) {
                    indentLevel++;
                }
            });

            return result.join('\n');
        }

        return code.split('\n')
            .map(line => line.trimStart())
            .join('\n');
    };

    const formattedCode = formatXml(code);

    return (
        <Highlight
            theme={themes.vsDark}
            code={formattedCode}
            language={language}
        >
            {({ className, style, tokens, getLineProps, getTokenProps }) => (
                <pre className={className} style={{
                    ...style,
                    padding: '1rem',
                    borderRadius: '0.5rem',
                    overflow: 'auto',
                    backgroundColor: '#1e2030'
                }}>
                    {tokens.map((line, i) => (
                        <div key={i} {...getLineProps({ line })}>
                            {line.map((token, key) => (
                                <span key={key} {...getTokenProps({ token })} />
                            ))}
                        </div>
                    ))}
                </pre>
            )}
        </Highlight>
    );
};

export default CodeBlock;