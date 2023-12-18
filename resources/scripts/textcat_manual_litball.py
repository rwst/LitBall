from typing import List, Optional
import prodigy
from prodigy.components.loaders import JSONL
from prodigy.util import split_string
from prodigy.components.preprocess import split_sentences
import spacy

nlp = spacy.load("en_core_web_lg")


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

def add_sentence_ids_to_stream(stream):
    for i, example in enumerate(split_sentences(nlp, stream)):
        example["meta"]["sentence_id"] = f"{i}"
        yield example

# Recipe decorator with argument annotations: (description, argument type,
# shortcut, type / converter function called on value before it's passed to
# the function). Descriptions are also shown when typing --help.
@prodigy.recipe(
    "textcat.manual_litball",
    dataset=("The dataset to use", "positional", None, str),
    source=("The source data as a JSONL file", "positional", None, str),
    label=("One or more comma-separated labels", "option", "l", split_string),
    exclusive=("Treat classes as mutually exclusive", "flag", "E", bool),
    exclude=("Names of datasets to exclude", "option", "e", split_string),
)

def textcat_manual_litball(
        dataset: str,
        source: str,
        label: Optional[List[str]] = None,
        exclusive: bool = False,
        exclude: Optional[List[str]] = None,
):
    """
    Manually annotate categories that apply to a text. If more than one label
    is specified, categories are added as multiple choice options. If the
    --exclusive flag is set, categories become mutually exclusive, meaning that
    only one can be selected during annotation.
    """

    # Load the stream from a JSONL file and return a generator that yields a
    # dictionary for each example in the data.
    stream = JSONL(source)
    stream = add_sentence_ids_to_stream(stream)

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
    }
