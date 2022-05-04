from flask import Flask, Blueprint, request, jsonify
    
def create_app():
    app = Flask("__main__")
    return app

