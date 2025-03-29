import { Highlight, themes } from 'prism-react-renderer';

interface CodeBlockProps {
    code: string;
    language: string;
}

const CodeBlock = ({ code, language }: CodeBlockProps) => {
    return (
        <div className="code-block-wrapper">
            <Highlight
                theme={themes.vsDark}
                code={code}
                language={language}
            >
                {({ className, style, tokens, getLineProps, getTokenProps }) => (
                    <pre
                        className={`${className} p-4 rounded-md text-sm overflow-x-auto`}
                        style={{
                            ...style,
                            backgroundColor: '#1e1e1e',
                            color: '#d4d4d4',
                        }}
                    >
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
        </div>
    );
};

export default CodeBlock;