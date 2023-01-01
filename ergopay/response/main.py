from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import SQL_functions

app = FastAPI()
origins = ["*"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class Item(BaseModel):
    txId: str


@app.get("/")
async def root():
    return {"message": "Hello World"}


@app.get("/response/{task_id}")
async def response(task_id):
    try:
        return {"tx_hash": SQL_functions.query_response_table(task_id)[1]}
    except Exception as e:
        print(e)
        return {"tx_hash": "None"}


@app.post("/txId")
async def faucet_api(item: Item, param: str):
    SQL_functions.write_to_response_table("response", "response", str(param), str(item.txId))
    return {"Success": True}
