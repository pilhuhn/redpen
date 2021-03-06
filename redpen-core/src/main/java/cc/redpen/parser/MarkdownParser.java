/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.parser;

import cc.redpen.RedPenException;
import cc.redpen.model.Document;
import cc.redpen.parser.markdown.ToFileContentSerializer;
import cc.redpen.tokenizer.RedPenTokenizer;
import org.pegdown.Extensions;
import org.pegdown.ParsingTimeoutException;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parser for Markdown format.<br>
 * <p>
 * Markdown Syntax @see http://daringfireball.net/projects/markdown/
 */
class MarkdownParser extends BaseDocumentParser {

    private PegDownProcessor pegDownProcessor = new PegDownProcessor(
            Extensions.HARDWRAPS
                    + Extensions.AUTOLINKS
                    + Extensions.FENCED_CODE_BLOCKS
                    + Extensions.TABLES);

    MarkdownParser() {
        super();
    }

    @Override
    public Document parse(InputStream inputStream, Optional<String> fileName, SentenceExtractor sentenceExtractor, RedPenTokenizer tokenizer)
            throws RedPenException {
        Document.DocumentBuilder documentBuilder = Document.builder(tokenizer);
        fileName.ifPresent(documentBuilder::setFileName);

        StringBuilder fullText = new StringBuilder();
        List<Integer> lineLengths = new ArrayList<>();

        try (PreprocessingReader br = createReader(inputStream)) {
            String line;
            int charCount = 0;
            while ((line = br.readLine()) != null) {
                fullText.append(line).append("\n");
                // TODO surrogate pair ?
                charCount += line.length() + 1;
                lineLengths.add(charCount);
            }
            documentBuilder.setPreprocessorRules(br.getPreprocessorRules());
        } catch (IOException e) {
            throw new RedPenException(e);
        }

        try {
            // TODO create documentBuilder after parsing... overhead...
            RootNode rootNode = pegDownProcessor.parseMarkdown(fullText.toString().toCharArray());
            ToFileContentSerializer serializer = new ToFileContentSerializer(documentBuilder, lineLengths, sentenceExtractor);
            serializer.toFileContent(rootNode);
        } catch (ParsingTimeoutException e) {
            throw new RedPenException("Failed to parse timeout: ", e);
        }
        return documentBuilder.build();
    }
}
