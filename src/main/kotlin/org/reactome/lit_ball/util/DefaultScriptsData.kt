package org.reactome.lit_ball.util

    object DefaultScriptsData {
        val scriptMap = mapOf<String, String>("sentence_labels.json" to """["Background", "Aim", "Methods-Lab", "Methods-Clinic", "Methods-Computational", "Methods-Other", "Result", "Conclusion", "Limitation", "Future Work", "Importance"]""",
"textcat_manual_litball.py" to """from typing import List, Optional
import prodigy
from prodigy.components.loaders import JSONL
from prodigy.util import split_string


# Helper functions for adding user provided labels to annotation tasks.
def add_label_options_to_stream(stream, labels):
    options = [{"id": label, "text": label} for label in labels]
    for task in stream:
        task["options"] = options
        yield task

def add_labels_to_stream(stream, labels):
    for task in stream:
        task["label"] = labels[0]
        yield task

# Recipe decorator with argument annotations: (description, argument type,
# shortcut, type / converter function called on value before it's passed to
# the function). Descriptions are also shown when typing --help.
@prodigy.recipe(
    "textcat.manual",
    dataset=("The dataset to use", "positional", None, str),
    source=("The source data as a JSONL file", "positional", None, str),
    label=("One or more comma-separated labels", "option", "l", split_string),
    exclusive=("Treat classes as mutually exclusive", "flag", "E", bool),
    exclude=("Names of datasets to exclude", "option", "e", split_string),
)

def textcat_manual(
        dataset: str,
        source: str,
        label: Optional[List[str]] = None,
        exclusive: bool = False,
        exclude: Optional[List[str]] = None,
):
    longstringdelimiterreplacement
    Manually annotate categories that apply to a text. If more than one label
    is specified, categories are added as multiple choice options. If the
    --exclusive flag is set, categories become mutually exclusive, meaning that
    only one can be selected during annotation.
    longstringdelimiterreplacement

    # Load the stream from a JSONL file and return a generator that yields a
    # dictionary for each example in the data.
    stream = JSONL(source)

    #Add labels to each task in stream
    has_options = len(label) > 1
    if has_options:
        stream = add_label_options_to_stream(stream, label)
    else:
        stream = add_labels_to_stream(stream, label)

    return {
        "view_id": "choice" if has_options else "classification",  # Annotation interface to use
        "dataset": dataset,  # Name of dataset to save annotations
        "stream": stream,  # Incoming stream of examples
        "exclude": exclude,  # List of dataset names to exclude
        "config": {  # Additional config settings, mostly for app UI
            "choice_style": "single" if exclusive else "multiple", # Style of choice interface
            "exclude_by": "input" if has_options else "task", # Hash value used to filter out already seen examples
        },
    }""",
"textcat_teach_litball.py" to """from typing import List, Optional
import spacy
from spacy.training import Example
import prodigy
from prodigy.components.loaders import JSONL
from prodigy.models.textcat import TextClassifier
from prodigy.models.matcher import PatternMatcher
from prodigy.components.sorters import prefer_uncertain
from prodigy.util import combine_models, split_string


# Recipe decorator with argument annotations: (description, argument type,
# shortcut, type / converter function called on value before it's passed to
# the function). Descriptions are also shown when typing --help.
@prodigy.recipe(
    "textcat.teach",
    dataset=("The dataset to use", "positional", None, str),
    spacy_model=("The base model", "positional", None, str),
    source=("The source data as a JSONL file", "positional", None, str),
    label=("One or more comma-separated labels", "option", "l", split_string),
    patterns=("Optional match patterns", "option", "p", str),
    exclude=("Names of datasets to exclude", "option", "e", split_string),
)
def textcat_teach(
        dataset: str,
        spacy_model: str,
        source: str,
        label: Optional[List[str]] = None,
        patterns: Optional[str] = None,
        exclude: Optional[List[str]] = None,
):
    longstringdelimiterreplacement
    Collect the best possible training data for a text classification model
    with the model in the loop. Based on your annotations, Prodigy will decide
    which questions to ask next.
    longstringdelimiterreplacement
    labels = label
    # Load the stream from a JSONL file and return a generator that yields a
    # dictionary for each example in the data.
    stream = JSONL(source)

    # Load the spaCy model
    nlp = spacy.load(spacy_model)

    # Specify the name of the classifier pipeline.
    name = "textcat_multilabel"

    # Initialize classification pipeline from scratch (using a dummy training example) or from the base model if available.
    if name not in nlp.pipe_names:
        pipe = nlp.add_pipe(name)
        # dummy doc
        doc = nlp.make_doc("hello")
        # dummy weights
        cats = {label: 0.5 for label in labels}
        pipe.initialize(get_examples=lambda: [Example.from_dict(doc, {"cats":cats})])
    else:
        pipe = nlp.get_pipe(name)

    # Initialize Prodigy's text classifier model, which outputs
    # (score, example) tuples
    model = TextClassifier(nlp, labels, name)

    if patterns is None:
        # No patterns are used, so just use the model to suggest examples
        # and only use the model's update method as the update callback
        predict = model
        update = model.update
    else:
        # Initialize the pattern matcher and load in the JSONL patterns.
        # Set the matcher to not label the highlighted spans, only the text.
        matcher = PatternMatcher(
            nlp,
            prior_correct=5.0,
            prior_incorrect=5.0,
            label_span=False,
            label_task=True,
        )
        matcher = matcher.from_disk(patterns)
        # Combine the NER model and the matcher and interleave their
        # suggestions and update both at the same time
        predict, update = combine_models(model, matcher)

    # Use the prefer_uncertain sorter to focus on suggestions that the model
    # is most uncertain about (i.e. with a score closest to 0.5). The model
    # yields (score, example) tuples and the sorter yields just the example
    stream = prefer_uncertain(predict(stream))

    return {
        "view_id": "classification",  # Annotation interface to use
        "dataset": dataset,  # Name of dataset to save annotations
        "stream": stream,  # Incoming stream of examples
        "update": update,  # Update callback, called with batch of answers
        "exclude": exclude,  # List of dataset names to exclude
        "config": {"lang": nlp.lang},  # Additional config settings, mostly for app UI
    }""",
) }